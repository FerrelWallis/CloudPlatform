#!/usr/bin/env perl
use warnings;
use strict;

die "perl $0 <ensembl|gencode> <junction.bed> <bp> \n" unless @ARGV eq 3;

&showtime("Start...");
my $in = shift;
my %gene;
open GTF, $in or die $!;
while(<GTF>){
	chomp;
	my @tmp = split;
	next if(/^#/);
	my ($gid) = $_ =~ /gene_id "([^;]+)";/;
	my ($tid) = $_ =~ /transcript_id "([^;]+)";/;
	if($tmp[2] =~ /exon/){
		push @{$gene{$gid}{$tid}{$tmp[0]}}, "$tmp[3]\t$tmp[4]";
	}
}
close GTF;

my %hash;
foreach my $g(keys %gene){
	foreach my $t(keys %{$gene{$g}}){
		foreach my $chr(keys %{$gene{$g}{$t}}){
			my $flag = 0;
			my $end;
			foreach my $str(sort @{$gene{$g}{$t}{$chr}}){
				if($flag == 0){
					$end = (split /\t/, $str)[1];
					$flag = 1;
				}else{
					my $f_end = (split /\t/, $str)[0];
					$hash{$chr}{$end}{$f_end} = 0;
					$end = (split /\t/, $str)[1];
				}
			}
		}
	}
}
&showtime("GTF is done..."); 

my $junc = shift;
#my ($id) = $junc =~ /map_(\w+)/;
open OUT1, ">$junc.novel" or die $!;
open OUT2, ">$junc.ref" or die $!;
my $bp = shift; 
#$bp ||= 0;
open JUNC, $junc or die $!;
while(<JUNC>){
	chomp;
	next if($. == 1);
	my @tmp = split;
	my $flag = 0;
	my ($block_s, $block_e) = (split /,/, $tmp[10])[0,1];
	my $intron_s = $tmp[1] + $block_s;
	my $intron_e = $tmp[2] - $block_e + 1;
	foreach my $s(keys %{$hash{$tmp[0]}}){
		foreach my $e(keys %{$hash{$tmp[0]}{$s}}){
			if(abs($intron_s - $s) <= $bp and abs($intron_e - $e) <= $bp){
				$flag = 1;
				print OUT2 "$tmp[0]\t$tmp[1]\t$tmp[2]\t$tmp[4]\t$tmp[5]\t$block_s\t$block_e\n";
				delete $hash{$tmp[0]}{$s}{$e};
				last;
			}
		}
	}
	if($flag eq 1)
	{
		next;
	}else{
		print OUT1 "$tmp[0]\t$tmp[1]\t$tmp[2]\t$tmp[4]\t$tmp[5]\t$block_s\t$block_e\n";
	}

}
close JUNC;
&showtime("Junctions filtering is done...");


sub showtime(){
	my ($str) = @_;
	my $time = localtime;
	print STDERR "$str\t$time\n";
}
