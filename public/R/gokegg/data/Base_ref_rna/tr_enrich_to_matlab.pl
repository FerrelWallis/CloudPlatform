#!usr/bin/perl -w

use strict;

die "usage:perl $0 <enrich.xls> <pvalue> <out.enrich.xls>\n" unless @ARGV == 3;

open A,$ARGV[0];

my %rela;my %kegg;	open OUT,">$ARGV[2]";
my $head=<A>;
chomp $head;	my @head=split /\t+/,$head;	my $p1=join "\t",@head[0..5];
while(<A>){
	chomp;next if(/^#/);next if(/^-/);
	my @a=split /\t+/;    next if(@a < 1);    my $inf=join "\t",@a[2..$#a];
	next if($a[5] > $ARGV[1]);	next if($a[1] eq "p");
	my $merge=join "\t",@a[0..5];
	if($a[7] eq "biological_process"){
		print OUT "$a[0]\tBP\t1\t$a[5]\n";
	}
	if($a[7] eq "cellular_component"){
		print OUT "$a[0]\tCC\t2\t$a[5]\n";
	}
	if($a[7] eq "molecular_function"){
		print OUT "$a[0]\tMF\t3\t$a[5]\n";
	}
}
close A;
