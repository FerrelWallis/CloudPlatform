#!usr/bin/perl -w

use strict;

die "usage:perl $0 <umi.stat> <align.txt> \n" unless @ARGV == 2;

open A,$ARGV[0];	open B,$ARGV[1];
my %reads;
while(<A>){
	chomp;my @a=split;	$reads{$a[0]}=$a[2];
}
close A;
print "ID\tclean-reads\tmapped-reads\tmapped-rate(%)\n";
while(<B>){
	chomp;my @a=split;	open IN,$a[1];
	while(my $inf=<IN>){
		chomp $inf;my @b=split /\s+/,$inf;
		if($inf=~/All_Mapped_Reads:/){
			my @c=split /\(/,$b[1];
			my @d=split /%/,$c[1];
			my $reads=int($reads{$a[0]}*$d[0]/100);
			print "$a[0]\t$reads{$a[0]}\t$reads\t";
			printf "%.2f\n",100*$reads/$reads{$a[0]};
		}
	}
	close IN;
}
