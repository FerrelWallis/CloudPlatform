#!usr/bin/perl -w

use strict;

die "sam\tfpkm pathway\nusage:perl $0 <fpkm.list> <output prefix> <gene or tran func alter>\n" unless @ARGV == 2 || @ARGV == 3;

open A,$ARGV[0];
open O1,">$ARGV[1].txt";	open O2,">$ARGV[1].gct";
my @sam;my %tar;	my $num=0;
while(<A>){
	chomp;my @b=split /\s+/;
	push @sam,$b[0];
	open IN,$b[1];<IN>;	
	while(my $inf=<IN>){
		chomp $inf;my @a=split /\s+/,$inf;	$tar{$a[0]}{$b[0]}=$a[-4];	
	}
	close IN;
}
close A;
my $head=join "\t",@sam;	my $sam=@sam;
print O1 "Gene_id\t$head\n";	
foreach my $k1(sort keys %tar){
	print O1 "$k1";	$num++;	
	foreach my $k2(@sam){
		print O1 "\t$tar{$k1}{$k2}";
	}
	print O1 "\n";
}
close O1;
print O2 "#1.2\n$num\t$sam\n";print O2 "NAME\tdescription\t$head\n";
foreach my $k1(sort keys %tar){
	print O2 "$k1\tna";
	foreach my $k2(@sam){
		print O2 "\t$tar{$k1}{$k2}";
	}
	print O2 "\n";
}
close O2;

if(@ARGV == 3){
	open B,$ARGV[2];	open O1,">$ARGV[1].anno.xls";	my %func;
	while(<B>){
		chomp;my @a=split /\t+/;
		$func{$a[0]}=$a[1];
	}
	close B;
	print O1 "Gene_id\t$head\tfunction\n";
	foreach my $k1(sort keys %tar){
		print O1 "$k1";
		foreach my $k2(@sam){
			print O1 "\t$tar{$k1}{$k2}";
		}
		my $type="_";
		if(exists $func{$k1}){	$type=$func{$k1};}
		print O1 "\t$type\n";
	}
	close O1;
}
